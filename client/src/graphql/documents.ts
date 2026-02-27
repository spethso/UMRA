/**
 * GraphQL query and mutation documents used across the UMRA client.
 *
 * @module graphql/documents
 */

import { gql } from '@apollo/client/core'

/** Fetches the list of available risk analyzers. */
export const ANALYZERS_QUERY = gql`
  query Analyzers {
    analyzers {
      analyzerId
      displayName
      sourceUrl
    }
  }
`

/** Submits clinical inputs for prostate-cancer risk analysis. */
export const ANALYZE_MUTATION = gql`
  mutation AnalyzeProstateCancerRisk($input: ProstateCancerRiskInput!, $analyzerIds: [String!], $storeResult: Boolean) {
    analyzeProstateCancerRisk(input: $input, analyzerIds: $analyzerIds, storeResult: $storeResult) {
      sessionId
      selectedAnalyzerIds
      stored
      result {
        analyzers {
          analyzerId
          displayName
          sourceUrl
          forwardedOnline
          success
          warning
          risk {
            noCancerRisk
            lowGradeRisk
            highGradeRisk
            cancerRisk
            grouped
          }
        }
        aggregate {
          noCancerRisk
          lowGradeRisk
          highGradeRisk
          cancerRisk
          basedOnAnalyzers
        }
      }
    }
  }
`

/** Deletes a stored session by its ID. */
export const DELETE_SESSION_MUTATION = gql`
  mutation DeleteSession($sessionId: String!) {
    deleteSession(sessionId: $sessionId)
  }
`

/** Retrieves a stored session (inputs + results) by its ID. */
export const SESSION_QUERY = gql`
  query Session($sessionId: String!) {
    session(sessionId: $sessionId) {
      sessionId
      selectedAnalyzerIds
      autoMode
      input {
        race
        age
        psa
        familyHistory
        dre
        priorBiopsy
        detailedFamilyHistoryEnabled
        fdrPcLess60
        fdrPc60
        fdrBc
        sdr
        pctFreePsaAvailable
        pctFreePsa
        pca3Available
        pca3
        t2ergAvailable
        t2erg
        snpsEnabled
        prostateVolumeCc
        mriPiradsScore
        dreVolumeClassCc
        gleasonScoreLegacy
        biopsyCancerLengthMm
        biopsyBenignLengthMm
        ukPostcode
        smokingStatus
        diabetesType
        manicSchizophrenia
        heightCm
        weightKg
        qcancerYears
      }
      result {
        analyzers {
          analyzerId
          displayName
          sourceUrl
          forwardedOnline
          success
          warning
          risk {
            noCancerRisk
            lowGradeRisk
            highGradeRisk
            cancerRisk
            grouped
          }
        }
        aggregate {
          noCancerRisk
          lowGradeRisk
          highGradeRisk
          cancerRisk
          basedOnAnalyzers
        }
      }
      createdAt
    }
  }
`
